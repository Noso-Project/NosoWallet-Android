## Version 1.0.9
- Fixed an error that caused the import wallet process to fail due to wrong file extension parsing
- Implemented a new Base64 decoding method for order signing to match nodes implementation
- "Send from all" checkbox gets unchecked now after an order is placed
- Order reference is now parsed to have spaces replaced by "_"
- The balance to send funds is now calculated by taking out the pending outgoings for each address
- Amount filter nows replace the first 0 digit after an order is placed (normal behavior)
- Summary zip file is now properly deleted