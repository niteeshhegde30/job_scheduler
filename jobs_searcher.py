import requests
import json

job_id = 5
# Define the Spring Boot API endpoint URL
url = f"http://localhost:8080/jobs/{job_id}"


try:
    # Send the POST request using the `json=` parameter
    response = requests.get(url)
    print("status code ", response.status_code )
    
    # Check the response from the Spring backend
    if response.status_code == 201 or response.status_code == 200:
        print("Success!")
        print("Response Body:", response.json()) # Assuming Spring returns JSON
    else:
        print(f"Failed with Status Code: {response.status_code}")
        print("Response Text:", json.dumps(response.text, indent=4))
        
except requests.exceptions.RequestException as e:
    print(f"An error occurred: {e}")
