"""Package the tagged, clean Git release plus a portable history bundle."""
from pathlib import Path
import io
import subprocess
import tempfile
import zipfile

root = Path(__file__).resolve().parents[1]
def git(*args):
    return subprocess.check_output(["git", *args], cwd=root)

if git("status", "--porcelain").strip():
    raise SystemExit("Commit all changes before packaging the release.")
if git("rev-parse", "HEAD") != git("rev-parse", "v1.0^{commit}"):
    raise SystemExit("HEAD must match the v1.0 release tag.")
name = "DAA_Assignment3_Alikhan_Yerbol_SE-2517"
output = root / (name + ".zip")
archive = git("archive", "--format=zip", "v1.0")
history = git("log", "--all", "--graph", "--decorate", "--oneline")
with tempfile.TemporaryDirectory(prefix="daa-submission-") as temporary:
    bundle = Path(temporary) / "repository.bundle"
    subprocess.run(["git", "bundle", "create", str(bundle), "--branches", "--tags"], cwd=root, check=True)
    subprocess.run(["git", "bundle", "verify", str(bundle)], cwd=root, check=True)
    with zipfile.ZipFile(io.BytesIO(archive)) as source, zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as target:
        for item in source.infolist():
            if not item.is_dir():
                target.writestr(name + "/" + item.filename, source.read(item.filename))
        target.writestr(name + "/submission/GIT_HISTORY.txt", history)
        target.write(bundle, name + "/submission/repository.bundle")
        target.writestr(name + "/submission/RESTORE_HISTORY.txt",
            "Repository: https://github.com/alikhanov0/daa-assignment3-graphs\n"
            "Branch: main\nTag: v1.0\n"
            "Restore history into a new folder:\n"
            "git clone -b main submission/repository.bundle restored-repository\n"
            "Remote feature branches and v1.0 are preserved in the bundle.\n")
with zipfile.ZipFile(output) as check:
    if check.testzip() is not None:
        raise SystemExit("Archive CRC check failed")
print(f"Created {output.name} ({output.stat().st_size:,} bytes), CRC verified")
