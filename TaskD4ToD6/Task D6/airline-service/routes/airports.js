const express = require('express');
const router = express.Router();

const validator = require('../middlewares/validators/airportValidator');
const controller = require('../controllers/airport');


router.get('/', validator.isPaginationQuery, controller.getAirports);
router.get('/:airportCode/schedules', validator.areScheduleParams, controller.getSchedulesByType);

module.exports = router;
