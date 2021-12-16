# Video action detection
Identifying actions from video. This app can currently detect 400 different type of actions.

# Source
This library has been inspired by pytorch video classification models.
[Doc repo](https://pytorch.org/vision/stable/models.html#torchvision.models.video.mc3_18).

## Integration
 1. For using videoactiondetection module in sample app, include the source code and add the below dependencies in entry/build.gradle to generate hap/support.har.

```
	implementation project(path: ':videoactiondetection')
```

 2. For using videoactiondetection module in separate application using har file, add the har file in the entry/libs folder and add the dependencies in entry/build.gradle file.

```
	implementation fileTree(dir: 'libs', include: ['*.har'])
```
 3. For using videoactiondetection module from a remote repository in separate application, add the below dependencies in entry/build.gradle file.

```
	implementation 'dev.applibgroup:videoactiondetection:1.0.0'
```

## Usage
 1. Initialise the constructor of VideoActionDetection with the getResourceManager() and getCacheDir() arguments.
 
 2. Use detectAction() to get the predicted action label from the video input.
Example:

```slice
    	VideoActionDetection mydetector = new VideoActionDetection(getResourceManager(), getCacheDir());
    	action = mydetector.detectAction(data, IMG_CHANNEL, IMG_DEPTH, MODEL_INPUT_HEIGHT, MODEL_INPUT_WIDTH);
```
Check the example app for more information.

## License

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

