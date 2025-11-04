/**
 * Formatter Utilities
 * Pure functions for address and data formatting
 */

let Address: any;
let Cell: any;

/**
 * Initialize formatters with required dependencies from @ton/core
 */
export function initFormatters(deps: { Address: any; Cell: any }) {
  Address = deps.Address;
  Cell = deps.Cell;
}

/**
 * Convert raw address (0:hex) to user-friendly format (UQ...)
 */
export function toUserFriendlyAddress(rawAddress: string | null, isTestnet: boolean): string | null {
  if (!rawAddress || !Address) return rawAddress;
  try {
    const addr = Address.parse(rawAddress);
    return addr.toString({ bounceable: false, testOnly: isTestnet });
  } catch (e) {
    console.warn('[formatters] Failed to parse address:', rawAddress, e);
    return rawAddress;
  }
}

/**
 * Convert base64 hash to hex
 */
export function base64ToHex(base64: string): string {
  try {
    const binaryString = atob(base64);
    let hex = '';
    for (let i = 0; i < binaryString.length; i++) {
      const hexByte = binaryString.charCodeAt(i).toString(16).padStart(2, '0');
      hex += hexByte;
    }
    return hex;
  } catch (e) {
    console.warn('[formatters] Failed to convert hash to hex:', base64, e);
    return base64;
  }
}

/**
 * Extract text comment from message body
 */
export function extractTextComment(messageBody: string | null): string | null {
  if (!messageBody || !Cell) return null;
  try {
    const cell = Cell.fromBase64(messageBody);
    const slice = cell.beginParse();
    
    // Check if it starts with 0x00000000 (text comment opcode)
    const opcode = slice.loadUint(32);
    if (opcode === 0) {
      return slice.loadStringTail();
    }
    return null;
  } catch (e) {
    return null;
  }
}

/**
 * Serialize date to ISO string
 */
export function serializeDate(value: unknown): string | null {
  if (!value) return null;
  if (value instanceof Date) {
    return value.toISOString();
  }
  if (typeof value === 'string' || typeof value === 'number') {
    const date = new Date(value);
    if (!isNaN(date.getTime())) {
      return date.toISOString();
    }
  }
  return null;
}

/**
 * Normalize hex string (ensure it starts with 0x)
 */
export function normalizeHex(hex: string): string {
  if (!hex) return hex;
  return hex.startsWith('0x') ? hex : `0x${hex}`;
}

/**
 * Convert hex string to Uint8Array
 */
export function hexToBytes(hex: string): Uint8Array {
  const cleanHex = hex.startsWith('0x') ? hex.slice(2) : hex;
  const bytes = new Uint8Array(cleanHex.length / 2);
  for (let i = 0; i < cleanHex.length; i += 2) {
    bytes[i / 2] = parseInt(cleanHex.substr(i, 2), 16);
  }
  return bytes;
}

/**
 * Convert Uint8Array to hex string
 */
export function bytesToHex(bytes: Uint8Array): string {
  return Array.from(bytes)
    .map(b => b.toString(16).padStart(2, '0'))
    .join('');
}
