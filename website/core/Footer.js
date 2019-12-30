const React = require('react');

class Footer extends React.Component {
  docUrl(doc) {
    const baseUrl = this.props.config.baseUrl;
    const docsUrl = this.props.config.docsUrl;
    const docsPart = `${docsUrl ? `${docsUrl}/` : ''}`;
    const langPart = `${this.props.language ? `${this.props.language}/` : ''}`;
    return `${baseUrl}${docsPart}${langPart}${doc}`;
  }

  pageUrl(doc, language) {
    const baseUrl = this.props.config.baseUrl;
    return baseUrl + (language ? `${language}/` : '') + doc;
  }

  render() {
    return (
      <footer className="nav-footer" id="footer" style={{ backgroundColor: this.props.config.colors.gray }}>
        <section className="sitemap">
          <div>
            <h5>Docs</h5>
            <a href={this.docUrl('installation')}>Installation</a>
          </div>
          <div>
            <h5>Community</h5>
          </div>
          <div>
            <h5>More</h5>
            <a href={this.props.config.repoUrl}>GitHub</a>
          </div>
        </section>
        <section className="copyright">{this.props.config.copyright}</section>
      </footer>
    );
  }
}

module.exports = Footer;
