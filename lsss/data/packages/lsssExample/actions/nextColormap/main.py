# --- Begin generated header ---
import os
import sys
sys.path.append(os.path.normpath(os.path.dirname(os.path.realpath(__file__)) + '/../../../../include'))
import lsss
# --- End generated header ---

allColormaps = lsss.get('/lsss/module/ColorBarModule/colormaps')

currentColormap = lsss.get('/lsss/module/ColorBarModule/colormap')['value']
currentIndex = allColormaps.index(currentColormap)

nextIndex = (currentIndex + 1) % len(allColormaps)
nextColormap = allColormaps[nextIndex]

lsss.post('/lsss/module/ColorBarModule/colormap', json={'value': nextColormap})
