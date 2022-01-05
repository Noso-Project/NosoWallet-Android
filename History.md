## Version 1.0.11
- General code clean up, removed unused libraries and repos from gradle project
- Fixed server deletion that prevented the first node to be deleted without selecting other first
- Now the log is cleared after 1MB size is reached
- Now any change to the server/nodes list is applied immediately
- Block and brach information is now incluided in the log
- Wallet Sync is now displayed correctly if no nodes were reached
- Wallet import using QR is now implemented
- Button to generate QR code for wallet addresses

## Version 1.0.9
- Fixed an error that caused the import wallet process to fail due to wrong file extension parsing
- Implemented a new Base64 decoding method for order signing to match nodes implementation
- "Send from all" checkbox gets unchecked now after an order is placed
- Order reference is now parsed to have spaces replaced by "_"
- The balance to send funds is now calculated by taking out the pending outgoings for each address
- Amount filter nows replace the first 0 digit after an order is placed (normal behavior)
- Summary zip file is now properly deleted

## Version 1.0.8
- Implemented new layout management to fit better small screens (using dimens.xml)
- Fixed write/read permissions request that were causing the app to crash
- Fixed the amount filter, caused to pass wrong value to order
- Fixed Summary retrieveing method, zip header was being write incorrectly
- Now summary files are deleted after sync as well
- Balance update is now triggered after a new block is sync
- Incoming/Outgoing update is triggered after new pendings are sync
- Added the "Send from all" option to use funds from all address in the current transaction
- Changed design for the amount filter from "x" to "!" when amount is invalid (not enough funds)
- Removed unused library for zip management (wasn't need anymore)

## Version 1.0.6
- Log report implemented, log file is saved now to device storage path: /Android/data/com.s7evensoftware.nosowallet/files/NOSODATA/LOGS/error_log.txt
- Time format changed, remove the AM/PM label
- Minor layout changes

## Version 1.0.5
- Fixed back button behavior in the settings dialog
- Removed error message for "export" wallet when action is cancelled by the user
- Fixed library compatibility with android 6 that was preventing the app from running