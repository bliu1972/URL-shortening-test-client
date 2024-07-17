# URL-shortening-test-client
URL shortener test client

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technologies Used](#technologies-used)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Running the Application](#running-the-application)
- [Contributing](#contributing)
- [License](#license)

## Overview

The URL Shortener test client a simple application that makes load calls to URL Shortener Service

## Features

- Generate load calls to URL Shortener Service for encode and decode

## Technologies Used

- Java 17
- Spring Boot
- Maven

## Getting Started

### Prerequisites

- Java 17
- Maven
- Git

### Installation

1. **Clone the repository:**

    ```sh
    git clone https://github.com/bliu1972/URL-shortening-test-client.git
    cd URL-shortening
    ```

### Running the Application

1. **Run the application locally:**
    ```sh
    mvn exec:java -Dexec.mainClass="com.util.load.LoadTestClient"
    ```

## Contributing

Contributions are welcome! Please open an issue or submit a pull request for any changes.

1. **Fork the repository**
2. **Create a new branch**

    ```sh
    git checkout -b feature/new-feature
    ```

3. **Make your changes**
4. **Commit your changes**

    ```sh
    git commit -m "Add new feature"
    ```

5. **Push to the branch**

    ```sh
    git push origin feature/new-feature
    ```

6. **Open a pull request**

## License

None

