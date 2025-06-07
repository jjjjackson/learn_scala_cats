he# learn_scala_cats
Learn how to make an API server using Scala with the Cats framework.

# Target 
- [ ] Build an API Server with Scala and Cats
- [ ] Complete a simple restaurant ordering system
  - [ ] CREATE /order
  - [ ] GET /order/:id
  - [ ] DELETE /order/:id
  - [ ] GET /tables/:id
- [ ] Use Postgres as the database
- [ ] Include basic unit tests and integration tests

Response schema for `GET /tables/:id`
```json
{
  table_id: number,
  orders: [
    order: {
      id: string,
      items: [
        {
          id: string,
          name: string,
          amount: number,
          unit_price: number
        }
      ]
    }
  ],
  total_price: number,
}
```

Response schema for `GET /order/:id`
```json
{
  id: string,
  table_id: number,
  items: [
    {
      id: string,
      name: string,
      amount: number,
      unit_price: number
    }
  ]
}

```

# Database Schema

## table
| Column Name | Type   | Description     |
| ----------- | ------ | --------------- |
| id          | SERIAL | Primary key, table number |

## order
| Column Name | Type   | Description                |
| ----------- | ------ | -------------------------- |
| id          | UUID   | Primary key, order ID      |
| table\_id   | INTEGER| Foreign key, references `tables(id)` |

## order_items
| Column Name   | Type          | Description                    |
| ------------- | ------------- | ------------------------------ |
| id            | UUID          | Primary key, order item ID     |
| order\_id     | UUID          | Foreign key, references `orders(id)` |
| name          | TEXT          | Product name                   |
| amount        | INTEGER       | Quantity (must be > 0)         |
| unit\_price   | NUMERIC(10,2) | Unit price (>= 0)              |

## items
> Hard code this schema. ( Avoid to distract from the practice target )

| Column Name   | Type          | Description                    |
| ------------- | ------------- | ------------------------------ |
| table\_id     | INTEGER       | Primary key, references `tables(id)` |
| total\_price  | NUMERIC(10,2) | Total amount for the table, defaults to 0 |
