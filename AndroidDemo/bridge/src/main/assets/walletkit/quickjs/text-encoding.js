(function (global) {
  if (typeof global.TextEncoder === 'undefined') {
    class QuickJsTextEncoder {
      constructor() {}
      get encoding() {
        return 'utf-8';
      }
      encode(input = '') {
        const source = Array.from(String(input ?? ''));
        const bytes = [];
        for (const unit of source) {
          const codePoint = unit.codePointAt(0);
          if (codePoint <= 0x7f) {
            bytes.push(codePoint);
          } else if (codePoint <= 0x7ff) {
            bytes.push(0xc0 | (codePoint >> 6), 0x80 | (codePoint & 0x3f));
          } else if (codePoint <= 0xffff) {
            bytes.push(
              0xe0 | (codePoint >> 12),
              0x80 | ((codePoint >> 6) & 0x3f),
              0x80 | (codePoint & 0x3f),
            );
          } else {
            bytes.push(
              0xf0 | (codePoint >> 18),
              0x80 | ((codePoint >> 12) & 0x3f),
              0x80 | ((codePoint >> 6) & 0x3f),
              0x80 | (codePoint & 0x3f),
            );
          }
        }
        return Uint8Array.from(bytes);
      }
      encodeInto(input, destination) {
        const encoded = this.encode(input);
        const target = destination instanceof Uint8Array ? destination : new Uint8Array(destination);
        const length = Math.min(encoded.length, target.length);
        for (let index = 0; index < length; index++) {
          target[index] = encoded[index];
        }
        return { read: String(input ?? '').length, written: length };
      }
    }
    Object.defineProperty(QuickJsTextEncoder.prototype, 'encoding', { value: 'utf-8' });
    global.TextEncoder = QuickJsTextEncoder;
  }

  if (typeof global.TextDecoder === 'undefined') {
    class QuickJsTextDecoder {
      constructor(label = 'utf-8') {
        const normalized = (label || 'utf-8').toString().toLowerCase();
        if (normalized !== 'utf-8' && normalized !== 'utf8') {
          throw new RangeError('Only utf-8 TextDecoder is supported');
        }
      }
      get encoding() {
        return 'utf-8';
      }
      decode(input = new Uint8Array()) {
        let bytes;
        if (input instanceof Uint8Array) {
          bytes = input;
        } else if (input && typeof input === 'object') {
          if (typeof input.buffer !== 'undefined') {
            const byteOffset = input.byteOffset || 0;
            const byteLength = input.byteLength || input.length || 0;
            bytes = new Uint8Array(input.buffer, byteOffset, byteLength);
          } else {
            bytes = Uint8Array.from(input);
          }
        } else {
          bytes = new Uint8Array();
        }
        let result = '';
        let index = 0;
        while (index < bytes.length) {
          const byte1 = bytes[index++];
          if ((byte1 & 0x80) === 0) {
            result += String.fromCharCode(byte1);
            continue;
          }
          if ((byte1 & 0xe0) === 0xc0) {
            if (index >= bytes.length) {
              result += '\\uFFFD';
              break;
            }
            const byte2 = bytes[index++];
            if ((byte2 & 0xc0) !== 0x80) {
              result += '\\uFFFD';
              continue;
            }
            const codePoint = ((byte1 & 0x1f) << 6) | (byte2 & 0x3f);
            if (codePoint < 0x80) {
              result += '\\uFFFD';
              continue;
            }
            result += String.fromCharCode(codePoint);
            continue;
          }
          if ((byte1 & 0xf0) === 0xe0) {
            if (index + 1 >= bytes.length) {
              result += '\\uFFFD';
              break;
            }
            const byte2 = bytes[index++];
            const byte3 = bytes[index++];
            if ((byte2 & 0xc0) !== 0x80 || (byte3 & 0xc0) !== 0x80) {
              result += '\\uFFFD';
              continue;
            }
            const codePoint = ((byte1 & 0x0f) << 12) | ((byte2 & 0x3f) << 6) | (byte3 & 0x3f);
            if (codePoint < 0x800) {
              result += '\\uFFFD';
              continue;
            }
            result += String.fromCharCode(codePoint);
            continue;
          }
          if ((byte1 & 0xf8) === 0xf0) {
            if (index + 2 >= bytes.length) {
              result += '\\uFFFD';
              break;
            }
            const byte2 = bytes[index++];
            const byte3 = bytes[index++];
            const byte4 = bytes[index++];
            if ((byte2 & 0xc0) !== 0x80 || (byte3 & 0xc0) !== 0x80 || (byte4 & 0xc0) !== 0x80) {
              result += '\\uFFFD';
              continue;
            }
            let codePoint =
              ((byte1 & 0x07) << 18) |
              ((byte2 & 0x3f) << 12) |
              ((byte3 & 0x3f) << 6) |
              (byte4 & 0x3f);
            if (codePoint < 0x10000 || codePoint > 0x10ffff) {
              result += '\\uFFFD';
              continue;
            }
            codePoint -= 0x10000;
            result += String.fromCharCode(
              0xd800 + ((codePoint >> 10) & 0x3ff),
              0xdc00 + (codePoint & 0x3ff),
            );
            continue;
          }
          result += '\\uFFFD';
        }
        return result;
      }
    }
    Object.defineProperty(QuickJsTextDecoder.prototype, 'encoding', { value: 'utf-8' });
    global.TextDecoder = QuickJsTextDecoder;
  }
})(typeof globalThis !== 'undefined' ? globalThis : this);
