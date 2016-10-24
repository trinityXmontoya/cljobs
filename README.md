# cljobs

Scrapes [Stack Overflow](stackoverflow.com/jobs) , [Indeed](www.indeed.com/jobs), and [Monster](https://www.monster.com/jobs) for Clojure jobs

### Assumptions
- Postgres is running on port 5432 and you've created a database 'cljobs':

  `create database cljobs;`
- following env vars

```
{:indeed-api-key INDEED-PUBLISHER-API-KEY
 :google-email YOUR-GOOGLE-EMAIL
 :google-pw YOUR-GOOGLE-PW
 :recipient-email WHO-TO-SEND-JOBS-EMAIL-TO}
```
