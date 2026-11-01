# Inventory Calculator (NeoForge 26.2)

Client-side arithmetic calculator displayed beside the player inventory.

## Features
- Inventory-side toggle button.
- Compact, draggable calculator panel.
- Mouse keypad and keyboard input.
- Addition, subtraction, multiplication, division, decimals, unary negatives, and parentheses.
- Standard operator precedence and division-by-zero validation.
- Clear, backspace, Enter to calculate, and copy-result support.
- Remembers the last expression, result, and panel position in \`config/invcalculator.properties\`.
- Does not read, move, or modify inventory slots.

## Target
- Minecraft 26.2
- NeoForge 26.2.0.93
- Java 25

## Build
Run \`gradle build\`. The JAR is generated in \`build/libs/\`.
GitHub Actions builds the project and uploads the JAR as a workflow artifact.

## Controls
- Click the ÷ button beside the inventory to open or close the calculator.
- Drag the title bar to move the panel.
- Type numbers and operators while the calculator is open.
- Enter calculates; Backspace deletes; Delete clears; Escape closes.
- Ctrl+C or the Copy result label copies the current result.
