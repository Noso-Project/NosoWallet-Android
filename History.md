## Version 1.0.13
- Implemented the OrderID history, it can be access from the context menu (by long pressing on any address)
- NOSO logo is now added next to the NOSOmobile title
- Error Log file size limit is now reduced to 50KB to make it easier to handle
- All deleted addresses are now stored in the "erased.pkw" file in NOSODATA dir
- New error handler implemented to catch all unhandled errors
- New seed node added (23.94.21.83)

## Version 1.0.12
### IMPORTANT NOTE!
- This is the first release fully builded from github using actions which implements a signing method to be used for github and in local environments, this to use a unique cert, it will be helpful if in the future (Hopefully I'll be with you for much more time) someone else needs to take care of the repo. This means that the cert won't match with the previous versions, therefore you have to UNINSTALL the old version first, aka, data will be lost, please BACKUP first, please export your addresses so you can import them back after installing this version, this is the only time that we'll have to do this

### Changelog
- New developement cert implemented to have a unique signing method
- Customized address will now have priority over public address and will be shown in the address list
- Partially implemented the customization methods (still doesn't do anything but we are half way there)
- Now it prevents the deletion if there is only 1 address left

## Version 1.0.11
- Added option to delete addresses with a context menu by long pressing an address
- Fixed the send order method to match the node response (this was causing a request to be sent multiple times)

## Version 1.0.10
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
- Log report implemented, log file is saved now to device storage path: /Android/data/com.nosoproject.nosowallet/files/NOSODATA/LOGS/error_log.txt
- Time format changed, remove the AM/PM label
- Minor layout changes

## Version 1.0.5
- Fixed back button behavior in the settings dialog
- Removed error message for "export" wallet when action is cancelled by the user
- Fixed library compatibility with android 6 that was preventing the app from running
