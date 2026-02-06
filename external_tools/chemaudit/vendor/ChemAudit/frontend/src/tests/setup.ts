/**
 * Vitest Setup File
 *
 * Configures test environment with:
 * - DOM testing utilities from @testing-library/jest-dom
 * - Mock for RDKit.js WASM module
 * - Mock for window.matchMedia
 * - Cleanup between tests
 * - ThemeProvider wrapper for tests
 */
import '@testing-library/jest-dom';
import { vi, beforeAll, afterAll, afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';
import React from 'react';
import { render as rtlRender, type RenderOptions } from '@testing-library/react';
import { ThemeProvider } from '../contexts/ThemeContext';

// Run cleanup after each test
afterEach(() => {
  cleanup();
});

// Mock localStorage for theme persistence
const mockLocalStorage = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: vi.fn((key: string) => store[key] || null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value;
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key];
    }),
    clear: vi.fn(() => {
      store = {};
    }),
  };
})();

Object.defineProperty(window, 'localStorage', {
  value: mockLocalStorage,
});

// Mock RDKit.js module
// The actual RDKit module loads a ~15MB WASM file which isn't suitable for unit tests
const createMockMol = () => ({
  get_svg: vi.fn(() => '<svg><rect width="100" height="100"/></svg>'),
  get_svg_with_highlights: vi.fn(() => '<svg><rect width="100" height="100"/></svg>'),
  get_molblock: vi.fn(() => ''),
  get_smiles: vi.fn(() => 'CCO'),
  delete: vi.fn(),
});

const mockRDKitModule = {
  version: vi.fn(() => '2024.3.0'),
  get_mol: vi.fn(() => createMockMol()),
  get_mol_from_smiles: vi.fn(() => createMockMol()),
  get_mol_from_molblock: vi.fn(() => createMockMol()),
  prefer_coordgen: vi.fn(),
};

// Mock window.initRDKitModule
vi.stubGlobal('initRDKitModule', vi.fn().mockResolvedValue(mockRDKitModule));

// Mock window.RDKit (for already-loaded scenarios)
vi.stubGlobal('RDKit', mockRDKitModule);

// Mock matchMedia (for responsive components and theme detection)
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: query.includes('prefers-color-scheme: dark') ? false : false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// Mock ResizeObserver
class MockResizeObserver {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}

vi.stubGlobal('ResizeObserver', MockResizeObserver);

// Mock IntersectionObserver
class MockIntersectionObserver {
  constructor(callback: IntersectionObserverCallback) {
    this.callback = callback;
  }
  callback: IntersectionObserverCallback;
  root = null;
  rootMargin = '';
  thresholds = [];
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
  takeRecords = vi.fn(() => []);
}

vi.stubGlobal('IntersectionObserver', MockIntersectionObserver);

// Suppress console errors for expected test failures
const originalError = console.error;
beforeAll(() => {
  console.error = (...args: unknown[]) => {
    // Suppress React 18 act() warnings in tests
    if (typeof args[0] === 'string' && args[0].includes('act(...)')) {
      return;
    }
    originalError.call(console, ...args);
  };
});

afterAll(() => {
  console.error = originalError;
});

/**
 * Custom render function that wraps components in ThemeProvider
 */
function AllTheProviders({ children }: { children: React.ReactNode }) {
  return React.createElement(ThemeProvider, null, children);
}

function customRender(ui: React.ReactElement, options?: Omit<RenderOptions, 'wrapper'>) {
  return rtlRender(ui, { wrapper: AllTheProviders, ...options });
}

// Re-export everything from @testing-library/react
export * from '@testing-library/react';

// Override the render function
export { customRender as render };
