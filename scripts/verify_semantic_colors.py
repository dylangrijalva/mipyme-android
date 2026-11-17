"""Guard production components and screens against raw palette and literal color access."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PALETTE_ACCESS = re.compile(
    r"(?:Moshi\.colors|colors)\.(?:gray(?:Alpha)?\d+|background(?:100|200)|"
    r"(?:blue|red|amber|green|teal|purple|pink)\d{2,4})\b"
)
LITERAL_COLOR = re.compile(r"\bColor\s*\(|\bColor\.(?!Transparent\b|Unspecified\b)[A-Za-z_]\w*")
scanned = 0
for directory in [
    ROOT / "core/ui/src/main/kotlin/co/mipyme/ui/component",
    ROOT / "app/src/main/java/co/mipyme/screen",
]:
    for source in directory.rglob("*.kt"):
        text = source.read_text()
        assert not PALETTE_ACCESS.search(text), f"Raw palette access in {source}"
        assert not LITERAL_COLOR.search(text), f"Literal color in {source}"
        scanned += 1
print(f"Passed: checked {scanned} production component and screen files for palette access and literal colors.")
