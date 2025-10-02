# --- Begin generated header ---
import os
import sys
sys.path.append(os.path.normpath(os.path.dirname(os.path.realpath(__file__)) + '/../../../../include'))
import lsss
# --- End generated header ---

# Run an action in the lsssExample package:
lsss.post('/lsss/package/lsssExample/action/nextColormap/run')

# Run an action in the builtin lsss package:
lsss.post('/lsss/package/lsss/action/selectNextRegion/run')
