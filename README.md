Podax is a podcast downloader and player. It is heavily modeled after Google Listen, which is no longer in development.

Features include:

- keeping a list of subscriptions
- downloading new podcasts when they are available
- importing subscriptions from Google Reader
- a widget for easy pausing and resuming
- open source development at [Github](https://www.github.com/thasmin/Podax)

Building Podax
==============

Ant
---
This assumes both git and the Android SDK are installed and in the path.

- Find the id of the most recent SDK with "android list targets". Use this number where it says <id> below.
- cd to the directory above what will hold Podax.

1. git clone git://github.com/thasmin/Podax.git Podax
2. cd Podax
3. git submodule init
4. git submodule update
5. android update project -n Podax -p . --subprojects -t <id>
6. android update lib-project -p ViewPagerIndicator\library -t <id>
7. [ the same for any other submodule ... e.g., currently also
   ActionBarSherlock, drag-sort-listview, and Riasel ]
8. ant clean debug

Steps 4, 5, and 8 are required for each new pull from the github repo;
steps 6, and 7, if the are any changes in the step 4.

Eclipse
-------
The Android SDK and Eclipse with the ADT plugin must be installed. Go through steps 1 through 4 above.
5. In Eclipse, create a new project. Choose an Android project from existing code. Browse to the Podax directory, then deselect all and choose only Podax.
6. One by one, create new Android projects from existing code for the 4 library projects: ActionBarSherlock, ViewPagerIndicator, Riasel, and drag-sort-listview. If the project is named "library", then rename it to the proper name.
7. Right click on Podax and choose "Properties" at the bottom. Open the Android tab. Make sure that the libraries are pointing to the proper projects.
