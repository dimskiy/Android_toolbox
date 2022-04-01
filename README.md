# Android toolbox

Set of basic building blocks to build commonly-used app scenarios, such as:
* Recycler view list with nice animations on add\remove\update items, supporting items list filtering.
* Create proper list items padding when using Recycler View with complex layouts, such as GridLayout.
* Implement state-enabled data loading to show UI notification if data is loading 
  or there is some kind of error arose when fetching.
* Use data loading strategies with support of network and local cache to quickly show the last
  data value while the new one is being loading from the back-end, for instance.
  
## Add dependencies
1. Add to project' build dependencies:
```
allprojects {
    repositories {
        maven {
            url 'https://jitpack.io'
        }
    }
}
```
2. Add to the app's module:
```
dependencies {
    implementation 'com.github.dimskiy:Android_toolbox:VERSION'
}
```
Latest version:
[![](https://jitpack.io/v/dimskiy/Android_toolbox.svg)](https://jitpack.io/#dimskiy/Android_toolbox)