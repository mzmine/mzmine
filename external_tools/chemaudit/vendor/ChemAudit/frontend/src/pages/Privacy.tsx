import { motion } from 'framer-motion';
import { Shield, Database, Cookie, Eye, Lock, Server } from 'lucide-react';

/**
 * Privacy Policy Page
 * Transparent disclosure of data practices - purely functional, no tracking
 */
export function PrivacyPage() {
  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-8">
      {/* Header */}
      <motion.div
        className="text-center mb-12"
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-[var(--color-primary)]/20 to-[var(--color-accent)]/20 mb-4">
          <Shield className="w-8 h-8 text-[var(--color-primary)]" />
        </div>
        <h1 className="text-3xl sm:text-4xl font-bold text-[var(--color-text-primary)] tracking-tight font-display mb-3">
          Privacy Policy
        </h1>
        <p className="text-[var(--color-text-secondary)] max-w-2xl mx-auto">
          ChemAudit is designed with privacy in mind. We believe in transparency about how your data is handled.
        </p>
      </motion.div>

      {/* Content */}
      <motion.div
        className="space-y-8"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.1 }}
      >
        {/* TL;DR Section */}
        <div className="card-glass p-6 border-l-4 border-l-[var(--color-primary)]">
          <h2 className="text-lg font-semibold text-[var(--color-text-primary)] mb-3 flex items-center gap-2">
            <Eye className="w-5 h-5 text-[var(--color-primary)]" />
            TL;DR - The Short Version
          </h2>
          <ul className="space-y-2 text-[var(--color-text-secondary)]">
            <li className="flex items-start gap-2">
              <span className="text-green-500 mt-1">✓</span>
              <span><strong>No tracking</strong> - We don't use analytics, cookies for tracking, or any third-party services that monitor your activity</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-green-500 mt-1">✓</span>
              <span><strong>No accounts</strong> - No registration, no personal data collection</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-green-500 mt-1">✓</span>
              <span><strong>Local storage only</strong> - We only store your theme preference (light/dark) in your browser</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-green-500 mt-1">✓</span>
              <span><strong>No data retention</strong> - Your chemical structures are processed and immediately discarded</span>
            </li>
          </ul>
        </div>

        {/* What We Store */}
        <div className="card p-6">
          <h2 className="text-lg font-semibold text-[var(--color-text-primary)] mb-4 flex items-center gap-2">
            <Database className="w-5 h-5 text-[var(--color-primary)]" />
            What We Store
          </h2>
          <div className="bg-[var(--color-surface-sunken)] rounded-xl p-4">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 rounded-lg bg-[var(--color-primary)]/10 flex items-center justify-center">
                <Cookie className="w-5 h-5 text-[var(--color-primary)]" />
              </div>
              <div>
                <h3 className="font-medium text-[var(--color-text-primary)]">Theme Preference</h3>
                <p className="text-sm text-[var(--color-text-muted)]">localStorage key: <code className="bg-[var(--color-surface-elevated)] px-1.5 py-0.5 rounded text-xs">chemaudit-theme</code></p>
              </div>
            </div>
            <p className="text-sm text-[var(--color-text-secondary)]">
              This stores your display preference (light, dark, or system) so the interface looks the way you want it every time you visit.
              This is purely functional and contains no personal information.
            </p>
          </div>
        </div>

        {/* What We Don't Do */}
        <div className="card p-6">
          <h2 className="text-lg font-semibold text-[var(--color-text-primary)] mb-4 flex items-center gap-2">
            <Lock className="w-5 h-5 text-[var(--color-primary)]" />
            What We Don't Do
          </h2>
          <div className="grid gap-3">
            {[
              { icon: '🚫', text: 'No cookies for tracking or advertising' },
              { icon: '🚫', text: 'No Google Analytics or similar services' },
              { icon: '🚫', text: 'No third-party scripts that collect data' },
              { icon: '🚫', text: 'No user accounts or personal information collection' },
              { icon: '🚫', text: 'No storage of your chemical structures on our servers' },
              { icon: '🚫', text: 'No sharing of any data with third parties' },
            ].map((item, i) => (
              <div key={i} className="flex items-center gap-3 p-3 rounded-lg bg-[var(--color-surface-sunken)]">
                <span className="text-lg">{item.icon}</span>
                <span className="text-[var(--color-text-secondary)]">{item.text}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Data Processing */}
        <div className="card p-6">
          <h2 className="text-lg font-semibold text-[var(--color-text-primary)] mb-4 flex items-center gap-2">
            <Server className="w-5 h-5 text-[var(--color-primary)]" />
            Chemical Structure Processing
          </h2>
          <p className="text-[var(--color-text-secondary)] mb-4">
            When you submit a chemical structure for validation:
          </p>
          <ol className="space-y-3 text-[var(--color-text-secondary)]">
            <li className="flex items-start gap-3">
              <span className="w-6 h-6 rounded-full bg-[var(--color-primary)]/10 text-[var(--color-primary)] text-sm font-medium flex items-center justify-center flex-shrink-0">1</span>
              <span>Your structure is sent to our server for processing</span>
            </li>
            <li className="flex items-start gap-3">
              <span className="w-6 h-6 rounded-full bg-[var(--color-primary)]/10 text-[var(--color-primary)] text-sm font-medium flex items-center justify-center flex-shrink-0">2</span>
              <span>Validation, scoring, and standardization are performed in memory</span>
            </li>
            <li className="flex items-start gap-3">
              <span className="w-6 h-6 rounded-full bg-[var(--color-primary)]/10 text-[var(--color-primary)] text-sm font-medium flex items-center justify-center flex-shrink-0">3</span>
              <span>Results are returned to your browser immediately</span>
            </li>
            <li className="flex items-start gap-3">
              <span className="w-6 h-6 rounded-full bg-[var(--color-primary)]/10 text-[var(--color-primary)] text-sm font-medium flex items-center justify-center flex-shrink-0">4</span>
              <span>Your structure is discarded - nothing is stored or logged</span>
            </li>
          </ol>
        </div>

        {/* GDPR Compliance */}
        <div className="card p-6">
          <h2 className="text-lg font-semibold text-[var(--color-text-primary)] mb-4">
            GDPR Compliance
          </h2>
          <p className="text-[var(--color-text-secondary)] mb-3">
            Since ChemAudit does not collect, store, or process personal data, most GDPR requirements do not apply.
            The theme preference stored in localStorage is considered a "strictly necessary" functional preference
            and does not require consent under GDPR Article 6(1)(f).
          </p>
          <p className="text-[var(--color-text-secondary)]">
            If you wish to remove this preference, you can clear your browser's local storage for this site,
            or simply use your browser's privacy/incognito mode.
          </p>
        </div>

        {/* Contact */}
        <div className="card p-6 bg-gradient-to-br from-[var(--color-primary)]/5 to-[var(--color-accent)]/5">
          <h2 className="text-lg font-semibold text-[var(--color-text-primary)] mb-3">
            Questions?
          </h2>
          <p className="text-[var(--color-text-secondary)]">
            If you have any questions about this privacy policy, please open an issue on our{' '}
            <a
              href="https://github.com/Kohulan/ChemAudit"
              target="_blank"
              rel="noopener noreferrer"
              className="text-[var(--color-primary)] hover:underline font-medium"
            >
              GitHub repository
            </a>.
          </p>
        </div>

        {/* Last Updated */}
        <p className="text-center text-sm text-[var(--color-text-muted)]">
          Last updated: {new Date().toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}
        </p>
      </motion.div>
    </div>
  );
}
