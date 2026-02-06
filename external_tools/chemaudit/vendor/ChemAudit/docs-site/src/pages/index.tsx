import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import useBaseUrl from '@docusaurus/useBaseUrl';
import styles from './index.module.css';

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  const logoUrl = useBaseUrl('/img/logo.png');
  return (
    <header className={styles.heroBanner}>
      <div className="container">
        <img
          src={logoUrl}
          alt="ChemAudit Logo"
          className={styles.heroLogo}
        />
        <h1 className={styles.heroTitle}>
          <span className={styles.brandChem}>Chem</span>Audit
        </h1>
        <p className={styles.heroTagline}>{siteConfig.tagline}</p>
        <p className={styles.heroSubtitle}>
          Validate, standardize, and assess ML-readiness of chemical structures with 40+ checks,
          batch processing, and comprehensive scoring.
        </p>
        <div className={styles.buttons}>
          <Link
            className={clsx('clay-button clay-button-primary', styles.buttonSpacing)}
            to="/docs/getting-started/installation">
            Get Started
          </Link>
          <Link
            className="clay-button clay-button-accent"
            to="/docs/api/overview">
            API Reference
          </Link>
        </div>
      </div>
    </header>
  );
}

interface Feature {
  title: string;
  icon: string;
  description: string;
}

const FeatureList: Feature[] = [
  {
    title: 'Comprehensive Validation',
    icon: '01',
    description: '8+ structural checks including parsability, valence, aromaticity, and stereochemistry validation',
  },
  {
    title: 'Batch Processing',
    icon: '02',
    description: 'Process up to 10,000 molecules with real-time WebSocket progress tracking and partial failure handling',
  },
  {
    title: 'Structural Alerts',
    icon: '03',
    description: 'Screen against PAINS, BRENK, NIH, ZINC, and ChEMBL alert sets with 1500+ patterns',
  },
  {
    title: 'ML-Ready Scoring',
    icon: '04',
    description: 'Assess drug-likeness, ADMET predictions, NP-likeness, and aggregator likelihood scoring',
  },
  {
    title: 'Standardization',
    icon: '05',
    description: 'ChEMBL-compatible pipeline with salt stripping, tautomer canonicalization, and parent extraction',
  },
  {
    title: 'Database Lookup',
    icon: '06',
    description: 'Cross-reference PubChem, ChEMBL, and COCONUT natural products database',
  },
];

function Feature({title, icon, description}: Feature) {
  return (
    <div className={clsx('clay-card clay-card-hover', styles.featureCard)}>
      <div className={styles.featureIcon}>{icon}</div>
      <h3>{title}</h3>
      <p>{description}</p>
    </div>
  );
}

function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <h2 className={styles.featuresTitle}>Everything You Need for Chemical Structure Validation</h2>
        <div className={styles.featureGrid}>
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}

function StatsBar() {
  const stats = [
    '40+ Checks',
    '10K+ Molecules/Batch',
    '6 Export Formats',
    'Open Source',
  ];

  return (
    <section className={styles.statsBar}>
      {stats.map((stat, idx) => (
        <div key={idx} className={clsx('clay-card', styles.statPill)}>
          {stat}
        </div>
      ))}
    </section>
  );
}

export default function Home(): JSX.Element {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={`${siteConfig.title} - ${siteConfig.tagline}`}
      description="Validate, standardize, and assess ML-readiness of chemical structures with comprehensive checks and batch processing.">
      <HomepageHeader />
      <main>
        <HomepageFeatures />
        <StatsBar />
      </main>
    </Layout>
  );
}
