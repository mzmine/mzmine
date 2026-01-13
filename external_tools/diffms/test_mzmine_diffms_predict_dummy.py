import argparse
import json
import subprocess
import sys
import tempfile
from pathlib import Path


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--python", default=sys.executable)
    p.add_argument("--diffms-dir", required=True)
    p.add_argument("--checkpoint", required=True)
    p.add_argument("--device", default="mps")
    p.add_argument("--top-k", type=int, default=3)
    args = p.parse_args()

    runner = Path(__file__).resolve().parent / "mzmine_diffms_predict.py"
    if not runner.is_file():
        raise FileNotFoundError(runner)

    req = [
        {
            "rowId": 114,
            "formula": "C6H12O6",
            "mzs": [85.02895, 99.04460, 113.02390, 127.03955, 145.05010],
            "intensities": [0.3, 1.0, 0.15, 0.08, 0.05],
            "polarity": "POSITIVE",
        }
    ]

    with tempfile.TemporaryDirectory(prefix="mzmine_diffms_dummy_") as td:
        td = Path(td)
        in_path = td / "in.json"
        out_path = td / "out.json"
        in_path.write_text(json.dumps(req))

        cmd = [
            str(args.python),
            str(runner),
            "--diffms-dir",
            str(Path(args.diffms_dir).resolve()),
            "--checkpoint",
            str(Path(args.checkpoint).resolve()),
            "--input",
            str(in_path),
            "--output",
            str(out_path),
            "--device",
            args.device,
            "--top-k",
            str(args.top_k),
        ]

        r = subprocess.run(cmd, text=True, capture_output=True)
        if r.returncode != 0:
            sys.stderr.write(r.stdout)
            sys.stderr.write(r.stderr)
            return r.returncode

        out = json.loads(out_path.read_text())
        print(json.dumps(out, indent=2))
        return 0


if __name__ == "__main__":
    raise SystemExit(main())

