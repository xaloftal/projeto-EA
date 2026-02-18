import { Sequelize } from 'sequelize';
import dotenv from 'dotenv';

dotenv.config();





const sequelize = new Sequelize(
  process.env.DB_NAME || 'catchit',
  process.env.DB_USER || 'root',
  process.env.DB_PASSWORD || 'password',
    { 
        host: process.env.DB_HOST || 'localhost',
        port: parseInt(process.env.DB_PORT || '5432', 10),
        dialect: 'postgres',
        logging: false,
    }
);

export default sequelize;