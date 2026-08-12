import requests
import json

# Define the Spring Boot API endpoint URL
url = "http://localhost:8080/jobs/job"

# Create a Python dictionary representing the object
create_job_request = {
    "taskName": "simple_task",
    "scheduleType": "ONE_TIME",
    "scheduledAt": "20260814",
    # "cronExpression": None,
    # "parameters":None
}

try:
    # Send the POST request using the `json=` parameter
    response = requests.post(url, json=create_job_request)
    
    # Check the response from the Spring backend
    if response.status_code == 201 or response.status_code == 200:
        print("Success!")
        print("Response Body:", response.json()) # Assuming Spring returns JSON
    else:
        print(f"Failed with Status Code: {response.status_code}")
        print("Response Text:", json.dumps(response.text, indent=4))
        
except requests.exceptions.RequestException as e:
    print(f"An error occurred: {e}")
