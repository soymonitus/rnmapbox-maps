---
name: Setup/installation error
about: This template should be used for reporting bugs and defects with project setup
labels: 'bug-setup :beetle:'
assignees: ''
---


## Environment
- Dev OS: [e.g. OSX 11.0.1, Win10]

## Steps to reproduce

<!--- We don't troubleshoot existing projects, please reproduce the issue in a brand new project. If you can't then create a new working project and compare with the one you're having trouble with.  --->

```sh   
react-native init sample --version react-native@0.60.5
cd sample
npm install mothership/mapbox-react-native#main --save
# or released version `npm install @mothership/mapbox-react-native@8.0.0-rc1 --save`
react-native run-android
```

