local component = require("component")
local filesystem = require("filesystem")
local shell = require("shell")

local wasm = component.compiler

local function emcc(args)

    local inputFile = nil
    local outputFile = nil
    local extraArgs = {}
    local isCpp = false
    
    local i = 1
    while i <= #args do
        if args[i] == "-o" then
            i = i + 1
            outputFile = args[i]
        else

            local a = args[i]
			if not (a:sub(1,1) == "-") and (a:match("%.c$")) then
				inputFile = a
			else
				table.insert(extraArgs, a)
			end
        end
        i = i + 1
    end
    
    if not inputFile then
        io.stderr:write("emcc: no input file\n")
        return 1
    end
    

    if not outputFile then
        outputFile = inputFile:gsub("%.[^%.]+$", "") .. ".wasm"
    end
    

    inputFile = shell.resolve(inputFile)
    outputFile = shell.resolve(outputFile)
    

    local inFs, inMount = filesystem.get(inputFile)
    if not inFs then
        io.stderr:write("emcc: cannot find filesystem for " .. inputFile .. "\n")
        return 1
    end
    local inRelPath = inputFile:sub(#inMount + 1)
    if inRelPath:sub(1,1) ~= "/" then inRelPath = "/" .. inRelPath end
    

    local outFs, outMount = filesystem.get(outputFile)
    if not outFs then

        local parent = outputFile:match("^(.*)/[^/]*$") or "/"
        outFs, outMount = filesystem.get(parent)
        if not outFs then
            io.stderr:write("emcc: cannot find filesystem for output " .. outputFile .. "\n")
            return 1
        end
    end
    local outRelPath = outputFile:sub(#outMount + 1)
    if outRelPath:sub(1,1) ~= "/" then outRelPath = "/" .. outRelPath end
    

    local token = wasm.compile(inFs.address, inRelPath, table.unpack(extraArgs))
    io.write("Compiling... token: " .. token .. "\n")
    

    while true do
        local status = wasm.getStatus(token)
        if status == "DONE" then
            break
        elseif status == "FAILED" then
			local errMsg = wasm.getError(token)
			if errMsg and #errMsg > 0 then
				io.stderr:write("em++: compilation failed\n")
				io.stderr:write(errMsg .. "\n")
			else
				io.stderr:write("em++: compilation failed\n")
			end
			return 1
        end
        os.sleep(1)
    end
    

    local ok = wasm.getResult(token, outFs.address, outRelPath)
    if ok then
        io.write("Output: " .. outputFile .. "\n")
        return 0
    else
        io.stderr:write("emcc: failed to write output\n")
        return 1
    end
end


local args = {...}
if #args == 0 then
    io.write("Usage: emcc <input> [-o output] [args...]\n")
    os.exit(0)
end

os.exit(emcc(args))