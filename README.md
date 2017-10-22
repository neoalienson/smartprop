# smartprop
Smart Properties Hackathon Repo

## Android App
The Android App is under folder app
Please create a resource file with following string name and its value,
1. machine_learning_key
2. faceSubscription_key
3. subscription_key

example:

    <?xml version="1.0" encoding="utf-8"?>
    <resources>
	<string name="machine_learning_host"><!-- put the url of your machine learning web service here, e.g., https://ussouthcentral.services.azureml.net/workspaces/9dae8315bc4d4a3a9e92f498df9af737/services/8d6447734fdb4dbfa89b966120597cca/execute?api-version=2.0&amp;format=swagger --></string>
        <string name="machine_learning_key"><!-- put your machine learning key here --></string>
        <string name="faceSubscription_key"><!-- put your face detection subscription key here --></string>
        <string name="subscription_key"><!-- put your emotion detection subscription key here --></string>
    </resources>

## Web App
Start a python local webserver or host it to somewhere

    cd smartmanager
    python -m SimpleHttpServer

## Resources
https://drive.google.com/drive/folders/0B2chQRrROUgKNDA1MUV0X1ZoLUE

## Project Page
http://www.hackathon.io/projects/17448

## Hackaton Page
http://www.hackathon.io/smartone-hackathon-smart-properties

## References
http://www.hackathon.io/smart-inspector

## Licence
Icon: https://www.shareicon.net/license/flaticon-basic-license
