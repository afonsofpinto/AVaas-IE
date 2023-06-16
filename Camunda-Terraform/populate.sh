camundaHost=$(cat camunda_addr.tmp)

function sendPOST() {
  local json=$1
  local path=$2
  echo $json
  curl -w "\n" -X POST -H "Content-Type: application/json" \
  -d "$json" "http://$camundaHost:8080/engine-rest$path"
}

# create users

sendPOST '{"profile":
    {"id": "user",
      "firstName":"John",
      "lastName":"Doe",
      "email":"a@a"},
      "credentials":
      {"password":"123"}
    }' "/user/create"

sendPOST '{"profile":
    {"id": "employee",
      "firstName":"Afonso",
      "lastName":"Pintarolas",
      "email":"a@a"},
      "credentials":
      {"password":"123"}
    }' "/user/create"

sendPOST '{"profile":
    {"id": "manufacturer",
      "firstName":"Diogo",
      "lastName":"Pintarolas",
      "email":"a@a"},
      "credentials":
      {"password":"123"}
    }' "/user/create"

sendPOST '{"profile":
    {"id": "developer",
      "firstName":"Pedro",
      "lastName":"Pedrocas",
      "email":"a@a"},
      "credentials":
      {"password":"123"}
    }' "/user/create"

# Authorize access to tasklist

sendPOST '{
  "type" : 1,
  "permissions": ["ACCESS"],
  "userId": "*",
  "groupId": null,
  "resourceType": 0,
  "resourceId": "tasklist"
}' "/authorization/create"