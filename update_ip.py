#!/usr/bin/env python3
"""
Script to replace old IP addresses with new IP addresses in all application.yaml files
across microservices directories.

Usage:
    python3 update_ip.py --old-ip 192.168.1.100 --new-ip 192.168.1.200
    python3 update_ip.py --old-ip 10.0.0.5 --new-ip 10.0.0.10 --dry-run
    python3 update_ip.py --old-ip localhost --new-ip 127.0.0.1
"""

import os
import sys
import re
import argparse
from pathlib import Path
from typing import List, Tuple
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class IPUpdater:
    """Class to handle IP address updates in YAML configuration files."""

    def __init__(self, old_ip: str, new_ip: str, dry_run: bool = False):
        """
        Initialize the IP updater.

        Args:
            old_ip: The old IP address to replace
            new_ip: The new IP address to use
            dry_run: If True, only show what would be changed without making changes
        """
        self.old_ip = old_ip
        self.new_ip = new_ip
        self.dry_run = dry_run
        self.files_found = []
        self.files_modified = []
        self.occurrences_replaced = 0

    def find_yaml_files(self, root_dir: str = ".") -> List[Path]:
        """
        Recursively find all application.yaml files in the directory structure.

        Args:
            root_dir: The root directory to search from

        Returns:
            List of Path objects pointing to application.yaml files
        """
        yaml_files = []
        root_path = Path(root_dir).resolve()

        logger.info(f"Searching for application.yaml files in: {root_path}")

        for path in root_path.rglob("application.yaml"):
            yaml_files.append(path)
            logger.debug(f"Found: {path}")

        logger.info(f"Found {len(yaml_files)} application.yaml file(s)")
        return yaml_files

    def validate_ip(self, ip: str) -> bool:
        """
        Validate IP address format (basic validation).

        Args:
            ip: IP address string to validate

        Returns:
            True if valid, False otherwise
        """
        # Allow hostnames like 'localhost', 'postgres', 'kafka', etc.
        if ip.isalnum() or '_' in ip or '-' in ip:
            return True

        # Validate IPv4 address format
        ipv4_pattern = r'^(\d{1,3}\.){3}\d{1,3}$'
        return bool(re.match(ipv4_pattern, ip))

    def update_file(self, file_path: Path) -> Tuple[int, bool]:
        """
        Update IP addresses in a single YAML file.

        Args:
            file_path: Path to the YAML file

        Returns:
            Tuple of (number of replacements, was file modified)
        """
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()

            original_content = content

            # Replace old IP with new IP (case-sensitive, whole word matching)
            # This regex ensures we match the IP as a complete token
            pattern = r'\b' + re.escape(self.old_ip) + r'\b'
            replacement_count = len(re.findall(pattern, content))

            if replacement_count > 0:
                new_content = re.sub(pattern, self.new_ip, content)

                if not self.dry_run:
                    with open(file_path, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    self.files_modified.append(file_path)
                    logger.info(f"✓ Updated {file_path}: {replacement_count} occurrence(s) replaced")
                else:
                    logger.info(f"[DRY RUN] Would update {file_path}: {replacement_count} occurrence(s)")

                return replacement_count, True
            else:
                logger.debug(f"No occurrences found in {file_path}")
                return 0, False

        except Exception as e:
            logger.error(f"Error processing {file_path}: {str(e)}")
            return 0, False

    def run(self, root_dir: str = ".") -> bool:
        """
        Main execution method to find and update all YAML files.

        Args:
            root_dir: The root directory to search from

        Returns:
            True if successful, False otherwise
        """
        # Validate input
        if not self.validate_ip(self.old_ip):
            logger.error(f"Invalid old IP format: {self.old_ip}")
            return False

        if not self.validate_ip(self.new_ip):
            logger.error(f"Invalid new IP format: {self.new_ip}")
            return False

        if self.old_ip == self.new_ip:
            logger.warning("Old IP and new IP are the same. No changes will be made.")
            return False

        # Find all YAML files
        yaml_files = self.find_yaml_files(root_dir)

        if not yaml_files:
            logger.warning("No application.yaml files found!")
            return False

        # Process each file
        logger.info(f"\n{'='*60}")
        logger.info(f"Replacing: {self.old_ip} → {self.new_ip}")
        logger.info(f"Dry Run: {self.dry_run}")
        logger.info(f"{'='*60}\n")

        total_replacements = 0
        for yaml_file in yaml_files:
            replacements, modified = self.update_file(yaml_file)
            total_replacements += replacements

        # Summary
        logger.info(f"\n{'='*60}")
        logger.info(f"Summary:")
        logger.info(f"  Files found: {len(yaml_files)}")
        logger.info(f"  Files modified: {len(self.files_modified)}")
        logger.info(f"  Total replacements: {total_replacements}")

        if self.dry_run:
            logger.info("  [DRY RUN MODE - No files were actually modified]")

        logger.info(f"{'='*60}\n")

        return True


def main():
    """Main entry point for the script."""
    parser = argparse.ArgumentParser(
        description='Update IP addresses in application.yaml files across microservices',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='''
Examples:
  # Replace 192.168.1.100 with 192.168.1.200
  python3 update_ip.py --old-ip 192.168.1.100 --new-ip 192.168.1.200

  # Dry run to see what would change
  python3 update_ip.py --old-ip 192.168.1.100 --new-ip 192.168.1.200 --dry-run

  # Replace hostname
  python3 update_ip.py --old-ip postgres-old --new-ip postgres-new

  # Search in specific directory
  python3 update_ip.py --old-ip 10.0.0.5 --new-ip 10.0.0.10 --dir ./Services
        '''
    )

    parser.add_argument(
        '--old-ip',
        required=True,
        help='Old IP address or hostname to replace'
    )
    parser.add_argument(
        '--new-ip',
        required=True,
        help='New IP address or hostname to use'
    )
    parser.add_argument(
        '--dir',
        default='.',
        help='Root directory to search from (default: current directory)'
    )
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Show what would be changed without making actual changes'
    )
    parser.add_argument(
        '--verbose',
        action='store_true',
        help='Enable verbose logging'
    )

    args = parser.parse_args()

    # Set logging level
    if args.verbose:
        logger.setLevel(logging.DEBUG)

    # Create updater and run
    updater = IPUpdater(
        old_ip=args.old_ip,
        new_ip=args.new_ip,
        dry_run=args.dry_run
    )

    success = updater.run(args.dir)

    # Exit with appropriate code
    sys.exit(0 if success else 1)


if __name__ == '__main__':
    main()