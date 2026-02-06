import { lazy, Suspense, useState, useEffect, useCallback } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { ErrorBoundary } from 'react-error-boundary';
import { Layout } from './components/layout/Layout';
import { MoleculeLoader } from './components/ui/MoleculeLoader';
import { SplashScreen } from './components/ui/SplashScreen';
import { ErrorFallback } from './components/error/ErrorFallback';
import { ConfigProvider } from './context/ConfigContext';
import { useRDKit } from './hooks/useRDKit';
import { cn } from './lib/utils';

// Eagerly import the home page to avoid Suspense flash after splash
import { SingleValidationPage } from './pages/SingleValidation';

// Lazy-loaded page components for code splitting (non-initial pages)
const BatchValidationPage = lazy(() =>
  import('./pages/BatchValidation').then(module => ({ default: module.BatchValidationPage }))
);
const AboutPage = lazy(() =>
  import('./pages/About').then(module => ({ default: module.AboutPage }))
);
const PrivacyPage = lazy(() =>
  import('./pages/Privacy').then(module => ({ default: module.PrivacyPage }))
);
const NotFoundPage = lazy(() =>
  import('./pages/NotFound').then(module => ({ default: module.NotFound }))
);

/**
 * Page loading fallback with molecule loader
 */
function PageLoaderFallback() {
  return (
    <div className="flex items-center justify-center h-64">
      <MoleculeLoader size="lg" text="Loading..." />
    </div>
  );
}


/**
 * RDKit error state component
 */
function RDKitErrorState({ error }: { error: string }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="card p-8 text-center max-w-md mx-auto"
    >
      <div className={cn(
        'w-14 h-14 mx-auto mb-4 rounded-2xl flex items-center justify-center',
        'bg-red-500/10 dark:bg-red-400/10'
      )}>
        <svg
          className="w-7 h-7 text-red-500 dark:text-red-400"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <circle cx="12" cy="12" r="10" />
          <path d="M15 9l-6 6M9 9l6 6" />
        </svg>
      </div>
      <h2 className="text-lg font-semibold text-red-600 dark:text-red-400 mb-2 font-display">
        Failed to load RDKit.js
      </h2>
      <p className="text-sm text-text-secondary">{error}</p>
    </motion.div>
  );
}

/**
 * App routes - rendered after RDKit is loaded
 */
function AppRoutes() {
  return (
    <AnimatePresence mode="wait">
      <Suspense fallback={<PageLoaderFallback />}>
        <Routes>
          <Route
            path="/"
            element={
              <motion.div
                key="single"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                transition={{ duration: 0.2 }}
              >
                <SingleValidationPage />
              </motion.div>
            }
          />
          <Route
            path="/batch"
            element={
              <motion.div
                key="batch"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                transition={{ duration: 0.2 }}
              >
                <BatchValidationPage />
              </motion.div>
            }
          />
          <Route
            path="/about"
            element={
              <motion.div
                key="about"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                transition={{ duration: 0.2 }}
              >
                <AboutPage />
              </motion.div>
            }
          />
          <Route
            path="/privacy"
            element={
              <motion.div
                key="privacy"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                transition={{ duration: 0.2 }}
              >
                <PrivacyPage />
              </motion.div>
            }
          />
          <Route
            path="*"
            element={
              <motion.div
                key="not-found"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                transition={{ duration: 0.2 }}
              >
                <NotFoundPage />
              </motion.div>
            }
          />
        </Routes>
      </Suspense>
    </AnimatePresence>
  );
}

/**
 * App content with splash screen - handles RDKit loading states
 */
function AppWithSplash() {
  const { loading, error } = useRDKit();
  const [showSplash, setShowSplash] = useState(true);
  const [minTimeElapsed, setMinTimeElapsed] = useState(false);

  // Ensure splash shows for minimum time (matches splash animation duration)
  useEffect(() => {
    const timer = setTimeout(() => setMinTimeElapsed(true), 1600);
    return () => clearTimeout(timer);
  }, []);

  const handleSplashComplete = useCallback(() => {
    if (!loading && minTimeElapsed) {
      setShowSplash(false);
    }
  }, [loading, minTimeElapsed]);

  // Auto-hide splash when both loading is done and min time elapsed
  useEffect(() => {
    if (!loading && minTimeElapsed) {
      // Small delay for smooth transition
      const timer = setTimeout(() => setShowSplash(false), 100);
      return () => clearTimeout(timer);
    }
  }, [loading, minTimeElapsed]);

  // Show splash screen while loading or during minimum display time
  if (showSplash) {
    return (
      <SplashScreen
        isVisible={true}
        onComplete={handleSplashComplete}
      />
    );
  }

  // Show error state
  if (error) {
    return (
      <Layout>
        <RDKitErrorState error={error} />
      </Layout>
    );
  }

  // RDKit loaded - show app
  return (
    <Layout>
      <AppRoutes />
    </Layout>
  );
}

/**
 * Main application component
 * Router wraps everything so Header can use useLocation
 */
function App() {
  return (
    <ConfigProvider>
      <Router
        future={{
          v7_startTransition: true,
          v7_relativeSplatPath: true,
        }}
      >
        <ErrorBoundary
          FallbackComponent={ErrorFallback}
          onError={(error, errorInfo) => {
            console.error('Uncaught error:', error, errorInfo);
          }}
          onReset={() => {
            window.location.href = '/';
          }}
        >
          <AppWithSplash />
        </ErrorBoundary>
      </Router>
    </ConfigProvider>
  );
}

export default App;
