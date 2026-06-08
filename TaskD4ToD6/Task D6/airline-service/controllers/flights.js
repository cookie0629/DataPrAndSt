const Flight = require('../models/Flight');

const getFlights = async (req, res, next) => {
  try {
    const { origin, destination, departureDate, fareConditions, transfers } = req.query;
    const flights = await Flight.getFlights({ origin, destination, departureDate, fareConditions, transfers });
    res.json(flights);
  } catch (error) {
    next(error);
  }
};

const checkIn = async (req, res, next) => {
  try {
    const { ticketNo } = req.body;
    const boardingPass = await Flight.checkIn({ ticketNo });
    res.status(201).json(boardingPass);
  } catch (error) {
    next(error);
  }
};

module.exports = { getFlights, checkIn };
