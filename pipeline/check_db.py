import os
import pandas as pd

# We can run a quick check via Java or Python mysql.connector or sqlite
# Since python might or might not have mysql-connector-python installed, let's check:
try:
    import mysql.connector
    print("mysql.connector available")
except ImportError:
    print("mysql.connector not installed")
