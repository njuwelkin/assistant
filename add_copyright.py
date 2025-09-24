#!/usr/bin/env python3
"""
Batch add MIT license copyright headers to all Java source files in the project.
"""
import os
import re

# MIT License template in English
COPYRIGHT_TEMPLATE = '''/**
 * MIT License
 * 
 * Copyright (c) 2023 illu@biubiu.org
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
'''

def has_copyright_header(file_content):
    """Check if the file already has a copyright header."""
    return "MIT License" in file_content and "illu@biubiu.org" in file_content

def add_copyright_to_file(file_path):
    """Add copyright header to a single Java file."""
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            content = file.read()
            
        # Skip if already has copyright header
        if has_copyright_header(content):
            print(f"✓ Already has copyright: {file_path}")
            return
        
        # Find the package statement
        package_match = re.search(r'package\s+[\w.]+;', content)
        
        if package_match:
            # Insert copyright before package statement
            new_content = COPYRIGHT_TEMPLATE + content
        else:
            # If no package statement, add copyright at the beginning
            new_content = COPYRIGHT_TEMPLATE + content
        
        # Write the modified content back
        with open(file_path, 'w', encoding='utf-8') as file:
            file.write(new_content)
            
        print(f"✓ Added copyright: {file_path}")
        
    except Exception as e:
        print(f"✗ Failed to process {file_path}: {str(e)}")

def main():
    """Main function to traverse project and add copyright headers."""
    project_root = os.path.dirname(os.path.abspath(__file__))
    
    # Find all Java files in the project
    java_files = []
    for root, _, files in os.walk(project_root):
        # Skip .git, build, and other non-source directories
        if any(skip_dir in root for skip_dir in ['.git', 'build', 'out', 'gradle', 'static']):
            continue
        
        # Add all .java files
        for file in files:
            if file.endswith('.java'):
                java_files.append(os.path.join(root, file))
    
    # Process each Java file
    total_files = len(java_files)
    print(f"Found {total_files} Java files to process.")
    
    for i, java_file in enumerate(java_files, 1):
        print(f"Processing {i}/{total_files}:", end=' ')
        add_copyright_to_file(java_file)
    
    print("\nAll files processed!")

if __name__ == "__main__":
    main()