import re
import unittest
from pathlib import Path

ROOT = Path(__file__).parents[1]


class PrototypeTests(unittest.TestCase):
    def test_all_navigation_routes_have_pages(self):
        script = (ROOT / "app.js").read_text(encoding="utf-8")
        routes = re.findall(r"\['([a-z]+)','[^']+','[a-z]+'\]", script)
        self.assertGreaterEqual(len(routes), 6)
        for route in routes:
            self.assertRegex(script, rf"(?:^|\n){route}:\(\)=>")

    def test_html_has_accessibility_landmarks(self):
        html = (ROOT / "index.html").read_text(encoding="utf-8")
        for marker in ('class="skip-link"', '<nav', '<main id="content"', 'aria-live="polite"'):
            self.assertIn(marker, html)

    def test_brand_colors_and_focus_are_declared(self):
        css = (ROOT / "styles.css").read_text(encoding="utf-8").lower()
        for color in ("#09090f", "#ff267a", "#7c3aed"):
            self.assertIn(color, css)
        self.assertIn(":focus-visible", css)

    def test_no_external_assets_or_runtime_dependencies(self):
        html = (ROOT / "index.html").read_text(encoding="utf-8")
        self.assertNotRegex(html, r'https?://')


if __name__ == "__main__":
    unittest.main()
