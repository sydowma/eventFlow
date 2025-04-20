-- request.lua
-- This script configures the POST request for wrk

-- Set HTTP method
wrk.method = "POST"

-- Set request headers
wrk.headers["Content-Type"] = "application/json"

-- Define the JSON payload template
-- You can make this dynamic using Lua's string formatting, os.date(), math.random etc.
-- For simplicity, this example uses a mostly static payload.
-- Replace with field names matching your Event record: id, name, description, date
local id = "wrk-" .. math.random(1, 1000000) -- Example: generate a semi-random ID
local timestamp = os.date("!%Y-%m-%dT%H:%M:%SZ") -- UTC timestamp
wrk.body = string.format('{"id": "%s", "name": "like-event-wrk", "description": "Stress test with wrk", "date": "%s"}', id, timestamp)

-- The request() function is called by wrk for each request.
-- In this simple case, we just return the pre-configured request object.
request = function()
  -- If you need to generate a unique body *per request*, do it here and update wrk.body
  -- local unique_id = "req-" .. math.random(1, 10000000)
  -- wrk.body = string.format('{"id": "%s", ... }', unique_id, ...)
  return wrk.request()
end 