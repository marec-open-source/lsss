# --- Begin generated header ---
import os
import sys
sys.path.append(os.path.normpath(os.path.dirname(os.path.realpath(__file__)) + '/../../../../include'))
import lsss
# --- End generated header ---

# Get current zoom, and calculate the center ping.
zoom = lsss.get('/lsss/module/PelagicEchogramModule/zoom')
centerPingNumber = (zoom[0]['pingNumber'] + zoom[1]['pingNumber']) // 2

# 'Depths' is defined as an input parameter for this action.
depths = lsss.input['Depths']

# Add the layer boundaries.
for depth in depths:
    boundary = [{'pingNumber': centerPingNumber, 'z': depth}]
    lsss.post('/lsss/module/PelagicEchogramModule/horizontal-layer-boundary', json=boundary)
