# MetroWest HVAC Systems - Web App

Testing with veracode app again again again again again 

This web application is designed to meet the needs of MetroWest HVAC Systems, Inc. (hereinafter referred to as "the company").

The application provides access to key business features, accessible to users with role based access restrictions. The following lists are non-exhaustive, but give a broad overview of the roles and common actions that can be performed by that role, with increasing levels of access required for increasingly sensitive or dangerous operations:

---

 ##### Access level 0: `ANONYMOUS`

Users at this level are unauthenticated, and can only view public content such as basic company info, and access the login page

Example Endpoints
- `/` - main landing page
- `/about` - contains basic information about the company
- `/login` - a user may log in to become authenticated

---

##### Access level 1: `CUSTOMER`

Users at this level are authenticated as a customer of the company, and may access information about their own profile. They may also see data related any orders they may have placed in the past, and an ordering portal they may use to place new orders. Ordering related UI controllers will have access to database tables used for parts and service SKUs, as well as data relating to their own account. 

Example Endpoints
- `/profile` - user account/profile information
- `/orders` - UI for placing new orders
- `/orders/history` - Order history listing

---

##### Access level 2: `TECHNICIAN`

Users at this level are authenticated as an employee of the company, and may access information about their assigned tasks. Tasks will contain task-related information about parts, service SKUs, and addresses of customers where they will need to go in order to perform installation or repair services. Technicians do **not** have permissions to update or edit their personal info, as this is controlled by a manager.

Example Endpoints
- `/tasks` - in the `TECHNICIAN` role, lists currently assigned tasks
- `/tasks/history` - lists previously assigned and completed tasks
- `/tasks/[task_id]` - shows information about a specific task

---

##### Access level 3: `MANAGER`

Users at this level are authenticated as an employee of the company, and may access information about their direct reports. Managers can add and remove new users with the `TECHNICIAN` role, create new "tasks", and assign those tasks to a technician. Managers also are responsible for managing incoming orders from `CUSTOMER` users, acknowledging the orders and essentially converting them from orders to tasks. 

Example Endpoints
- `/orders/incoming` - lists currently pending orders
- `/tasks` - in the `MANAGER` role, lists ongoing and recently completed tasks 
- `/tasks/new` - presents a UI for creating new tasks

---

##### Access level 4: `ADMIN`

Users at this level are authenticated as an administrator, and have full access to all endpoints. There should be a very small number of users with this role (preferably at least two, but not more than four). Members of this group should typically include the company owner and the head of the company IT department, and perhaps one or two other critical employees _if needed_.   

Example Endpoints
- `/users` - lists all users
- `/users/new` - presents a UI for adding new users
- `/logs` - access internal logging messages for auditing purposes
