## Version 2.0.2
- I fixed a problem with the back button action (was causing the stack to overflow).
- Added the back button logic to return form History view or close the Send Funds block.
- Implemented the methods needed for the PoP Service that will run in the background to earn noso from the mobile wallet (the logic is still missing so is not enabled yet).
- Implemented validation of Addresses or Custom Alias for funds sending (now it won't let you continue if is invalid).
- Fixed the "Send From All" logic, now it won't allow you to enable this option if there are funds enough in the selected wallet or if there are not funds at all in the other addresses.
- Changed the design of the buttons on the import dialog (I left the test buttons and forgot to changed them before release).
- Pop logic is now fully Implemented: PoP will be sent to all pools in random intervals within the block time. Service will run in background and there is no need to keep phone's screen on, but you may want to add the app to the battery saver exceptions to ensure it works fine.