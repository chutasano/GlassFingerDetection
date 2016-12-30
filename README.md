##GlassFaceDetection + Recognition


TODOs:
- Test functionality
- better UI
- more documentation
- better naming scheme for trained faces
- save trained faces
- clean up code
- fix on crash camera disposal
- Faceselection mode highlight box color

Cool features:
1) actually frees the camera. OpenCV documentation does not cover this.
2) CVLibTools -> converts OpenCV mats to JavaCV mats, still needs testing
3) Merges OpenCV and JavaCV together


###Usage: run app

Face Detection + Recognition Mode: by default
- swipe left/right to change minimum face size (higher the faster)
- tap to exit
- long tap to switch to face training mode

Face Training mode:
- same as face detection
- short tap to save current frame and go to face selection mode
- must select eight faces
- long tap to exit/cancel

Face Selection mode:
- a static gray image frame with detected face rectangles
- swipe left/right to highlight the face you want to recognize (red rectangle)
- short tap to remember selected rectangle as a face
- TODO: allow cancellation


###To make Javacv work:

I had some issues implementing Javacv in AndroidStudio. Below is how I got it to work. No guarantees that it will work for you too because I myself encountered errors while following the documentations.

Add these to build.gradle for Module: app (or whatever name your main application is)

```
compile group: 'org.bytedeco', name: 'javacv-platform', version: '1.3'
compile group: 'org.bytedeco', name: 'javacv', version: '0.11'
```

then gradle sync.

Another issue might be a duplicated files copied error. Fix that by appending

```
packagingOptions {
        pickFirst 'duplicated item'
      }
```


in android { }. It should look like this, where the ... represent what it had before.

```
android {

    ...

    packagingOptions {
       pickFirst 'blabla'
   }
}
```

See my build.gradle for an example