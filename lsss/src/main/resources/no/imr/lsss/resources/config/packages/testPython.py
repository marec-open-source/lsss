from importlib.metadata import version, PackageNotFoundError
from platform import python_version

print('test-from-lsss')
print(python_version())
try:
    print(version('requests'))
except PackageNotFoundError:
    print('Not available')
