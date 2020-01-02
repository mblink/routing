// See https://docusaurus.io/docs/site-config for all the possible
// site configuration options.

const repoUrl = 'https://github.com/mblink/http4s-routing';

const siteConfig = {
  title: 'http4s routing', // Title for your website.
  tagline: 'Fast, simple, and type-safe forward and reverse routing for http4s',
  url: 'https://mblink.github.io', // Your website URL
  baseUrl: '/http4s-routing/', // Base URL for your project */
  // For github.io type URLs, you would set the url and baseUrl like:
  //   url: 'https://facebook.github.io',
  //   baseUrl: '/test-site/',

  // Used for publishing and more
  projectName: 'http4s-routing',
  organizationName: 'mblink',
  // For top-level user or org sites, the organization is still the same.
  // e.g., for the https://JoelMarcey.github.io site, it would be set like...
  //   organizationName: 'JoelMarcey'

  // For no header links in the top nav bar -> headerLinks: [],
  headerLinks: [
    { doc: 'installation', label: 'Docs' },
    { href: repoUrl, label: 'GitHub', external: true },
  ],

  /* path to images for header/footer */
  headerIcon: 'img/routing.png',
  favicon: 'img/routing.png',

  /* Colors for website */
  colors: {
    primaryColor: '#333333',
    secondaryColor: '#333333'
  },

  /* Custom fonts for website */
  /*
  fonts: {
    myFont: [
      "Times New Roman",
      "Serif"
    ],
    myOtherFont: [
      "-apple-system",
      "system-ui"
    ]
  },
  */

  // This copyright info is used in /core/Footer.js and blog RSS/Atom feeds.
  copyright: `Copyright © ${new Date().getFullYear()} BondLink`,

  customDocsPath: 'http4s-routing-docs/target/mdoc',

  highlight: {
    // Highlight.js theme to use for syntax highlighting in code blocks.
    theme: 'github',
  },

  // Add custom scripts here that would be placed in <script> tags.
  scripts: ['https://buttons.github.io/buttons.js'],

  // On page navigation for the current documentation page.
  onPageNav: 'separate',
  // No .html extensions for paths.
  cleanUrl: true,

  // Open Graph and Twitter card images.
  // ogImage: 'img/undraw_online.svg',
  // twitterImage: 'img/undraw_tweetstorm.svg',

  // For sites with a sizable amount of content, set collapsible to true.
  // Expand/collapse the links and subcategories under categories.
  // docsSideNavCollapsible: true,

  // Show documentation's last contributor's name.
  // enableUpdateBy: true,

  // Show documentation's last update time.
  // enableUpdateTime: true,

  // You may provide arbitrary config keys to be used as needed by your
  // template. For example, if you need your repo's URL...
  repoUrl,
};

module.exports = siteConfig;
