soot-infoflow-android
=====================
Change this folder name to be soot-infoflow-android


NU FlowDroid Deployment
=====================
- Make a project directory and enter the directory:
  - `mkdir AndroidDataFlow; cd AndroidDataFlow`
- In the project directory, clone the following repositories:
  - `git clone https://github.com/Sable/jasmin.git`
  - `git clone https://github.com/Sable/heros.git`
  - `git clone https://github.com/xph906/FlowDroidNew.git`
  - `git clone https://github.com/xph906/FlowDroidInfoflowNew.git`
  - `git clone https://github.com/xph906/SootNew.git`
- Change modified respostories' names:
  - `mv FlowDroidNew soot-infoflow-android`
  - `mv FlowDroidInfoflowNew soot-infoflow`
  - `mv SootNew soot`
- Install apktool, following the instructions in the following link, note its path as `apk-tool-path`:
  - https://ibotpeaches.github.io/Apktool/install/
- Install (all) Andrid platforms and note its path as `android-platform-path`
  - `android-platform-path` might ends with .../some-path/sdk/platforms/
- Create two directories in current path, note as `decompiled-apk-file-path` and `graph-output-path`:
  - `mkdir decompiled-apk-file-path`
  - `mkdir graph-output-path`
- In Eclipse, select AndroidDataFlow as new workspace path.
- In Eclipse, import the five repositories.
  - File -> Import.. -> Exsiting Projects into Workspace 
- In Eclipse, create a new Java Application Run:
  - Select Main class as `soot.jimple.infoflow.android.TestApps.Test`
  - Enter program arguments using the following template:
    - `apk-file-path` `android-platform-path` -pathalgo CONTEXTSENSITIVE --apktoolpath `apk-tool-path` --tmppath `decompiled-apk-file-path` --graphoutpath `graph-output-path`
