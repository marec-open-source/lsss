import os
import requests
import urllib.request

# Save echogram images for all data files

baseUrl = 'http://127.0.0.1:8000'
saveDir = 'C:\\temp\\echograms\\'

if not os.path.exists(saveDir):
    os.makedirs(saveDir)

files = requests.get(baseUrl + '/lsss/survey/config/unit/DataConf/files').json()
n = len(files)
for i in range(n):
    file = files[i]['file']
    print(str(i + 1) + '/' + str(n) + ': ' + file)
    requests.post(baseUrl + '/lsss/survey/config/unit/DataConf/files/selection', json={'firstIndex': i, 'lastIndex': i})
    requests.get(baseUrl + '/lsss/data/wait')
    urllib.request.urlretrieve(baseUrl + '/lsss/module/PelagicEchogramModule/image', saveDir + file + '.png')
