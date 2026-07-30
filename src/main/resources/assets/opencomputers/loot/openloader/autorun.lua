local filesystem = require("filesystem")  -- @pluto_warnings: disable-all
local shell = require("shell")

local args = {...}
shell.setPath(shell.getPath() .. ":/mnt/"..args[1].address:sub(1,3).."/bin/")

