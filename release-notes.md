## Version 1.0.12
### IMPORTANT NOTE!
- This is the first release fully builded from github using actions which implements a signing method to be used for github and in local environments, this to use a unique cert, it will be helpful if in the future (Hopefully I'll be with you for much more time) someone else needs to take care of the repo. This means that the cert won't match with the previous versions, therefore you have to UNINSTALL the old version first, aka, data will be lost, please BACKUP first, please export your addresses so you can import them back after installing this version, this is the only time that we'll have to do this

### Changelog
- New developement cert implemented to have a unique signing method
- Customized address will now have priority over public address and will be shown in the address list
- Partially implemented the customization methods (still doesn't do anything but we are half way there)
- Now it prevents the deletion if there is only 1 address left
