# YAML Navigation Extension

## Overview
<!-- Plugin description -->

The **YAML Navigation Extension** is a Intelij extension designed to enhance the development workflow by providing advanced code navigation features for YAML files. This extension allows users to navigate directly to imported YAML files and locate YAML anchors (`&` definitions) across multiple imported files. It's particularly helpful in managing complex YAML configurations with nested structures and reusable content.

## Key Features

-   **Navigate to Imported YAML Files**: Click on an `import` path in a YAML file to jump directly to the imported file.
-   **Find YAML Anchors**: Navigate to anchor definitions referenced by `*` within YAML files and their imports.
-   **Logging and Debugging**: Optional logging output to track the extension's actions and debug navigation behavior.

## YAML File Structure

To enable the extension to recognize and navigate imports and anchors, YAML files must be structured in a specific format with an `import` section followed by a `---` separator. This structure helps the extension distinguish between the import section and the main content of the YAML file.

### Required YAML Structure

-   The `import` section must be at the top of the YAML file.
-   Use the `---` separator to separate the `import` section from the rest of the YAML content.

**Example YAML Structure:**

```yaml
import:
    - '../config.yml'
    - '../file/config.yml'
---
# Main content of the YAML file goes below
someKey:
  anotherKey: *referenceToAnchor

&anchorName
  key: value
```

### How It Works

1. **Import Recognition**:

  - The extension scans the `import` section to identify and resolve file paths relative to the current document's directory.
  - It supports paths such as `'../filename.yml'`.

2. **Anchor Navigation**:
  - The extension looks for anchors defined with `&` in the imported files.
  - When a reference (`*anchor`) is clicked, it searches the specified files for the corresponding anchor and navigates to its location.

## Important Notes

-   **Custom YAML Parser Required**: This extension requires YAML files to follow a specific import structure. To handle these imports during actual YAML parsing, you may need to implement a custom YAML parser that supports recursive imports. Popular YAML parsers, such as `js-yaml`, do not natively support custom import handling. A custom implementation could involve reading the `import` section, recursively loading the files, and merging their content.
-   [PHP Example](#PHP-Example-for-Recursive-YAML-Import)

## Example YAML File with Imports and Anchors

**File: `main.yml`**

```yaml
import:
    - './components/part1.yml'
    - './components/part2.yml'
---
config:
  setting1: *part1Anchor
  setting2: *part2Anchor
```

**File: `components/part1.yml`**

```yaml
&part1Anchor
part1:
    key: value
```

**File: `components/part2.yml`**

```yaml
&part2Anchor
part2:
    key: anotherValue
```



## Future Enhancements

-   Improved navigation to specific lines or sections within imported files.
-   Better performance optimization for large files or multiple recursive imports.
-

## PHP Example for Recursive YAML Import

To effectively manage YAML files with recursive imports in PHP, you may need to implement a custom YAML parser. Below is an example of how you can recursively load YAML files and handle the `import` structure.

This example uses the `symfony/yaml` package to parse YAML files. Make sure to install it via Composer if you haven't already:

```php
composer require symfony/yaml

<?php

require 'vendor/autoload.php';

use Symfony\Component\Yaml\Yaml;

function loadYamlWithImports($filePath, $loadedFiles = [])
{
    // Avoid loading the same file multiple times
    if (in_array($filePath, $loadedFiles)) {
        return [];
    }

    // Mark the file as loaded
    $loadedFiles[] = $filePath;

    // Load the YAML file
    $content = file_get_contents($filePath);
    $data = Yaml::parse($content);

    // Check for imports
    if (isset($data['import'])) {
        foreach ($data['import'] as $importPath) {
            // Resolve relative paths
            $importFullPath = dirname($filePath) . '/' . $importPath;
            $importedData = loadYamlWithImports($importFullPath, $loadedFiles);
            // Merge imported data into the current data
            $data = array_merge($data, $importedData);
        }
        // Remove the import section after processing
        unset($data['import']);
    }

    return $data;
}

// Example usage
$mainYamlFile = 'path/to/your/main.yml';
$finalData = loadYamlWithImports($mainYamlFile);
print_r($finalData);
```


<!-- Plugin description end -->

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
