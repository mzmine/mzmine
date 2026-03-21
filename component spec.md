# Spec for a grouping tree view

I started refactoring the featureListsList and rawDataList in the mzmine main window from a
GroupableListView to a TreeView. (see MainWindowController)
The current GroupableListView is causing problems and adds a bunch of custom code we would like to
avoid.
With the MetadataTreeView i started a replacement, but it is not perfect yet.
The grouping should be more flexible than just metadata.
I think we need a new component or a wrapping system for the items in the tree view, that is
flexible.
Also it should be easily sortable.
Sorting should first sort level 1, then level 2, then level 3...
The new component or wrapper must be a templated type, as we want to use it for RawDataFile and
FeatureList

## Grouping options

### Common

- No grouping - All raw data files/feature lists are on the top level
- Custom grouping by the user. The user selects items in the tree view and groups them manually.
    - Ungrouped items stay on the top level

## RawDataFile specific

- Default is no grouping - all files on top level
- Metadata - Group all raw files by a metadata column. You should be able to select a metadata
  column similar to the MetadataHeaderColumn and all files with the same metadata value are grouped.
  The metadata value becomes the name of the group.
- All files that do not have a value in the metadata column, stay on the top level

## Feature list specific

- Group by raw data file. (default)
    - Feature lists with more than one raw data file are grouped under the common "Aligned feature
      lists" item.
- Group by latest applied processing step (see FeatureListAppliedMethod)
- Group by metadata of the raw data file
    - Only applies to feature lists that have only one raw data file
    - All feature lists with multiple raw data files should be grouped under the common "Aligned
      feature lists" item.

# Implementation specifics

- Drag & drop should be possible to:
    - sort items
    - add the File of a RawDataFile to the Dragboard, so we can drag the raw files into other UI
      components
- Dont change the MainWindowController too much. It will be moved to MVCI in the future, but should
  stay as is for now
- The selection of the grouping should be at the top of the component
- When the user applies a grouping by metadata, it should still be editable by him without editing
  the metadata.
    - Maybe we can snapshot the current grouping and silently move to a custom grouping as soon as
      the user starts to edit the metadata derived grouping (or processing step-derived grouping...)
- The placement in tabs in the main window should not be changed.
- Consider the following options:
    - create only a wrapper for the items that are embedded in the TreeItem
    - Extend a TreeView<T>
    - Create a new component that contains a tree view
    - If it is better to create a new MVCI for this because it would be too complex for a single
      class
    - If you have another idea, layout that idea to me


- Before implementing consult with me to clear any questions you have.