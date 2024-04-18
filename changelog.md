## New features

* (1.7.10) [#3524] Add support for reading Thaumcraft aspect information from Wands. (repo-alt)
* Improve OpenOS "package" implementation:
  * [#3447] Populate package.config, add support for the package.preload table. (RobertCochran)
  * Add support for the package.searchers table.

## Fixes/improvements

* [CVE-2024-31446] Fixed Lua virtual machine freeze involving xpcall().
* (1.12.2) [#3659] Fixed bug when programatically transferring fluids from specific tanks. (yut23)
* [#3664] Fixed client-side errors when using third-party mod energy integration on an integrated server.
* [#3677] Fixed crash when showing error containing a percent sign with the Analyzer item.
* [#3698] Fixed documentation for the Screen's "turnOn" and "turnOff" functions. (Hawk777, DCNick3)
* [#3663] Fixed response code/message information not being preserved for unsuccessful HTTP responses.
* [#3691] Improved documentation for software bundled with the "network" floppy. (Computerdores)
* [#3644] Improved forged packet protection with regards to configuring server racks. (Glease)
* [#3652] Updated French translation. (ff66theone)
* Updated GNU Unifont to 15.1.05.

## List of contributors

asie, Computerdores, ff66theone, Glease, Hawk777, repo-alt, RobertCochran, yut23
