Podax is a podcast downloader and player. It is heavily modeled after Google Listen, which is no longer in development.

Features include:

- keeping a list of subscriptions
- downloading new podcasts when they are available
- importing subscriptions from Google Reader
- a widget for easy pausing and resuming
- open source development at [Github](https://www.github.com/thasmin/Podax)

To get started developing Podax, you may need to rebuild the project files. You can do this by running *android sdk path*/tools/android update project -p *Podax path*. You will also need to import the submodule as projects in Eclipse. After updating the submodules, go to File -> Import -> Existing Projects into Workspace (under General), then choose the ActionBarSherlock directory. Repeat for ViewPagerIndicator. If Podax isn't compiling, go to its project properties, then Android, and readd the two libraries in the bottom section.
