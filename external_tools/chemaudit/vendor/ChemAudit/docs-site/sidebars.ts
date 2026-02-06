import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  docs: [
    { type: 'doc', id: 'intro', label: 'Introduction' },
    {
      type: 'category',
      label: 'Getting Started',
      collapsed: false,
      items: [
        'getting-started/installation',
        'getting-started/configuration',
        'getting-started/first-validation',
      ],
    },
    {
      type: 'category',
      label: 'User Guide',
      collapsed: false,
      items: [
        'user-guide/single-validation',
        'user-guide/batch-processing',
        'user-guide/structural-alerts',
        {
          type: 'category',
          label: 'Scoring',
          items: [
            'user-guide/scoring/overview',
            'user-guide/scoring/ml-readiness',
            'user-guide/scoring/drug-likeness',
            'user-guide/scoring/safety-filters',
            'user-guide/scoring/admet',
            'user-guide/scoring/np-likeness',
            'user-guide/scoring/scaffold-analysis',
            'user-guide/scoring/aggregator-likelihood',
          ],
        },
        'user-guide/standardization',
        'user-guide/database-integrations',
        'user-guide/exporting-results',
      ],
    },
    {
      type: 'category',
      label: 'API Reference',
      collapsed: true,
      items: [
        'api/overview',
        'api/authentication',
        'api/endpoints',
        'api/websocket',
        'api/error-handling',
        'api/rate-limits',
      ],
    },
    {
      type: 'category',
      label: 'Deployment',
      collapsed: true,
      items: [
        'deployment/docker',
        'deployment/production',
        'deployment/monitoring',
      ],
    },
    { type: 'doc', id: 'troubleshooting', label: 'Troubleshooting' },
  ],
};

export default sidebars;
