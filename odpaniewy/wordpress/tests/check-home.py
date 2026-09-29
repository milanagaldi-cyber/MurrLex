"""Check real WordPress-rendered storefront HTML in disposable CI."""
from html.parser import HTMLParser
import pathlib
import sys

class Page(HTMLParser):
    def __init__(self):
        super().__init__()
        self.meta = {}
        self.products = 0
        self.canonicals = []
    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == 'meta':
            self.meta.setdefault(attrs.get('name', attrs.get('property')), []).append(attrs.get('content', ''))
        if tag == 'li' and 'product' in attrs.get('class', '').split():
            self.products += 1
        if tag == 'link' and attrs.get('rel') == 'canonical':
            self.canonicals.append(attrs.get('href'))

text = pathlib.Path(sys.argv[1]).read_text()
page = Page()
page.feed(text)
assert page.products == 3, f'Expected three alternative products, got {page.products}'
assert len(page.meta.get('description', [])) == 1, 'Exactly one meta description'
assert page.meta.get('og:url') == ['http://127.0.0.1:8089/'], 'Canonical social URL without campaign parameters'
assert len(page.canonicals) == 1, 'WordPress native canonical remains singular'
assert any('noindex' in value for value in page.meta.get('robots', [])), 'VPN staging stays noindex'
assert 'id="wc-order-attribution-js"' in text, 'Native attribution script is enqueued'
assert 'coupon-code' in text and 'wiosna' in text.lower(), 'Selected active coupon is rendered'
assert 'Zobacz wszystkie produkty' in text, 'Full category remains accessible'
assert 'Fatal error' not in text and 'Warning:' not in text, 'No PHP errors in output'
print('PASS: real storefront HTML, SEO, privacy, product selection and attribution script')
