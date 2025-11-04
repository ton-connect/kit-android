/**
 * Validator Utilities
 * Pure functions for input validation
 */

/**
 * Validate that a value is not null/undefined
 */
export function required<T>(value: T | null | undefined, fieldName: string): T {
  if (value === null || value === undefined) {
    throw new Error(`${fieldName} is required`);
  }
  return value;
}

/**
 * Validate that a value is a non-empty string
 */
export function requiredString(value: unknown, fieldName: string): string {
  if (typeof value !== 'string' || value.length === 0) {
    throw new Error(`${fieldName} must be a non-empty string`);
  }
  return value;
}

/**
 * Validate that a value is a non-empty array
 */
export function requiredArray<T>(value: unknown, fieldName: string): T[] {
  if (!Array.isArray(value) || value.length === 0) {
    throw new Error(`${fieldName} must be a non-empty array`);
  }
  return value;
}

/**
 * Validate that a value is a valid mnemonic (12, 18, or 24 words)
 */
export function validateMnemonic(words: unknown, fieldName: string = 'mnemonic'): string[] {
  const arr = requiredArray<string>(words, fieldName);
  if (arr.length !== 12 && arr.length !== 18 && arr.length !== 24) {
    throw new Error(`${fieldName} must contain 12, 18, or 24 words`);
  }
  return arr;
}

/**
 * Validate that a value is a positive number
 */
export function validatePositiveNumber(value: unknown, fieldName: string): number {
  if (typeof value !== 'number' || value <= 0) {
    throw new Error(`${fieldName} must be a positive number`);
  }
  return value;
}

/**
 * Validate that a value is a valid address string
 */
export function validateAddress(value: unknown, fieldName: string = 'address'): string {
  const addr = requiredString(value, fieldName);
  // Basic validation - should be a non-empty string
  // More detailed validation happens in the core
  if (addr.length < 10) {
    throw new Error(`${fieldName} appears to be invalid (too short)`);
  }
  return addr;
}

/**
 * Validate optional string
 */
export function optionalString(value: unknown): string | undefined {
  if (value === null || value === undefined) return undefined;
  if (typeof value !== 'string') {
    throw new Error('Value must be a string if provided');
  }
  return value;
}

/**
 * Validate optional number
 */
export function optionalNumber(value: unknown): number | undefined {
  if (value === null || value === undefined) return undefined;
  if (typeof value !== 'number') {
    throw new Error('Value must be a number if provided');
  }
  return value;
}
