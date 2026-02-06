/**
 * SVG and HTML Sanitization Utilities
 *
 * Provides XSS protection for dynamically rendered content,
 * particularly SVG images from RDKit molecule rendering.
 */

import DOMPurify from 'dompurify';

/**
 * DOMPurify configuration for SVG sanitization.
 * Allows SVG-specific elements while blocking dangerous content.
 */
const SVG_PURIFY_CONFIG = {
  USE_PROFILES: { svg: true, svgFilters: true },
  ADD_TAGS: [
    'svg',
    'g',
    'path',
    'rect',
    'circle',
    'ellipse',
    'line',
    'polyline',
    'polygon',
    'text',
    'tspan',
    'defs',
    'clipPath',
    'mask',
    'use',
    'symbol',
    'linearGradient',
    'radialGradient',
    'stop',
    'pattern',
  ],
  ADD_ATTR: [
    'viewBox',
    'xmlns',
    'xmlns:xlink',
    'fill',
    'stroke',
    'stroke-width',
    'stroke-linecap',
    'stroke-linejoin',
    'stroke-dasharray',
    'stroke-dashoffset',
    'stroke-opacity',
    'fill-opacity',
    'opacity',
    'transform',
    'd',
    'x',
    'y',
    'x1',
    'y1',
    'x2',
    'y2',
    'cx',
    'cy',
    'r',
    'rx',
    'ry',
    'width',
    'height',
    'points',
    'font-family',
    'font-size',
    'font-weight',
    'text-anchor',
    'dominant-baseline',
    'class',
    'id',
    'clip-path',
    'mask',
    'href',
    'xlink:href',
    'offset',
    'stop-color',
    'stop-opacity',
    'gradientUnits',
    'gradientTransform',
    'patternUnits',
    'patternTransform',
  ],
  // Block potentially dangerous elements
  FORBID_TAGS: [
    'script',
    'iframe',
    'object',
    'embed',
    'form',
    'input',
    'button',
    'a',
    'foreignObject',
  ],
  // Block event handlers and dangerous attributes
  FORBID_ATTR: [
    'onclick',
    'onload',
    'onerror',
    'onmouseover',
    'onmouseout',
    'onfocus',
    'onblur',
    'onsubmit',
    'onreset',
    'onchange',
    'onkeydown',
    'onkeyup',
    'onkeypress',
  ],
};

/**
 * Suspicious patterns that might indicate malicious SVG content.
 * Used for logging/auditing purposes.
 */
const SUSPICIOUS_PATTERNS = [
  /<script/i,
  /javascript:/i,
  /on\w+\s*=/i, // Event handlers like onclick=, onerror=
  /<iframe/i,
  /<object/i,
  /<embed/i,
  /<foreignObject/i,
  /data:text\/html/i,
  /expression\s*\(/i, // CSS expression (IE)
  /url\s*\(\s*["']?javascript:/i, // JavaScript in url()
];

/**
 * Sanitize SVG content for safe rendering.
 *
 * Removes potentially dangerous elements and attributes while
 * preserving valid SVG structure for molecule visualization.
 *
 * @param svg - Raw SVG string to sanitize
 * @returns Sanitized SVG string safe for dangerouslySetInnerHTML
 */
export function sanitizeSvg(svg: string | null | undefined): string {
  if (!svg) {
    return '';
  }

  // Sanitize using DOMPurify with SVG-specific config
  const sanitized = DOMPurify.sanitize(svg, SVG_PURIFY_CONFIG);

  return sanitized;
}

/**
 * Check if SVG content contains suspicious patterns.
 *
 * This is useful for logging/auditing without blocking.
 * The actual blocking is handled by DOMPurify in sanitizeSvg().
 *
 * @param svg - SVG string to check
 * @returns Object with isSuspicious flag and list of matches
 */
export function detectSuspiciousSvg(svg: string | null | undefined): {
  isSuspicious: boolean;
  matches: string[];
} {
  if (!svg) {
    return { isSuspicious: false, matches: [] };
  }

  const matches: string[] = [];

  for (const pattern of SUSPICIOUS_PATTERNS) {
    if (pattern.test(svg)) {
      matches.push(pattern.source);
    }
  }

  return {
    isSuspicious: matches.length > 0,
    matches,
  };
}

/**
 * Sanitize HTML content for safe rendering.
 *
 * More restrictive than SVG sanitization.
 * Use for user-provided text that might contain HTML.
 *
 * @param html - Raw HTML string to sanitize
 * @returns Sanitized HTML string
 */
export function sanitizeHtml(html: string | null | undefined): string {
  if (!html) {
    return '';
  }

  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'span', 'br', 'p', 'ul', 'ol', 'li'],
    ALLOWED_ATTR: ['class'],
  });
}

/**
 * Escape HTML entities in a string.
 *
 * Use for text content that should be displayed literally,
 * not interpreted as HTML.
 *
 * @param text - Text to escape
 * @returns Escaped text safe for display
 */
export function escapeHtml(text: string | null | undefined): string {
  if (!text) {
    return '';
  }

  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
